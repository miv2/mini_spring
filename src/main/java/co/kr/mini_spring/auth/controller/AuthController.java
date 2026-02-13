package co.kr.mini_spring.auth.controller;

import co.kr.mini_spring.global.security.MemberAdapter;
import co.kr.mini_spring.auth.dto.request.TokenRefreshRequest;
import co.kr.mini_spring.auth.dto.response.TokenResponse;
import co.kr.mini_spring.global.common.response.ApiResponse;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 인증 관련 API 컨트롤러 (OAuth2 전용)
 * - 소셜 로그인은 /oauth2/authorization/{provider}를 통해 처리
 * - 이 컨트롤러는 토큰 갱신 및 로그아웃만 담당
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "인증", description = "토큰 관리 (OAuth2 전용)")
public class AuthController {

    private final AuthService authService;
    private final co.kr.mini_spring.global.security.JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    @Operation(summary = "토큰 재발급", description = "RefreshToken을 검증해 새로운 Access/Refresh 토큰을 발급합니다.")
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody TokenRefreshRequest request,
                                             HttpServletRequest httpRequest,
                                             HttpServletResponse httpResponse) {
        log.info("[TokenRefresh] 요청");
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshToken = resolveRefreshTokenFromCookie(httpRequest);
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            return ApiResponse.fail(ResponseCode.TOKEN_NOT_FOUND);
        }

        TokenResponse response = authService.refreshToken(refreshToken);
        setRefreshCookies(httpRequest, httpResponse, response);
        return ApiResponse.success(ResponseCode.SUCCESS, response);
    }

    @Operation(summary = "로그아웃", description = "로그인된 사용자의 RefreshToken을 폐기합니다.")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal MemberAdapter memberAdapter,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        if (memberAdapter == null) {
            return ApiResponse.fail(ResponseCode.UNAUTHENTICATED);
        }
        log.info("[Logout] 요청 email={}", memberAdapter.getUsername());
        blacklistAccessToken(request);
        authService.logout(memberAdapter.getUsername());
        clearAuthCookies(request, response);
        return ApiResponse.success();
    }

    private void clearAuthCookies(HttpServletRequest request, HttpServletResponse response) {
        boolean secure = isSecureRequest(request);
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String proto = request.getHeader("X-Forwarded-Proto");
        return proto != null && proto.equalsIgnoreCase("https");
    }

    private void blacklistAccessToken(HttpServletRequest request) {
        String token = resolveAccessTokenFromRequest(request);
        if (token == null || token.isBlank()) {
            return;
        }
        LocalDateTime expiresAt = jwtTokenProvider.getExpiresAt(token);
        long ttlSeconds = Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
        if (ttlSeconds <= 0) {
            return;
        }
        stringRedisTemplate.opsForValue().set("bl:access:" + token, "1", Duration.ofSeconds(ttlSeconds));
    }

    private String resolveAccessTokenFromRequest(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring("Bearer ".length());
        }
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private String resolveRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void setRefreshCookies(HttpServletRequest request, HttpServletResponse response, TokenResponse tokenResponse) {
        boolean secure = isSecureRequest(request);
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", tokenResponse.getAccessToken())
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(java.time.Duration.between(java.time.LocalDateTime.now(), tokenResponse.getAccessTokenExpiresAt()).getSeconds())
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokenResponse.getRefreshToken())
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(java.time.Duration.between(java.time.LocalDateTime.now(), tokenResponse.getRefreshTokenExpiresAt()).getSeconds())
                .build();
        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }
}
