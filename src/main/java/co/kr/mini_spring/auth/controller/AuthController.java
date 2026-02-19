package co.kr.mini_spring.auth.controller;

import co.kr.mini_spring.auth.dto.request.TokenRefreshRequest;
import co.kr.mini_spring.auth.dto.response.TokenResponse;
import co.kr.mini_spring.auth.service.AuthService;
import co.kr.mini_spring.global.common.response.ApiResponse;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.global.security.MemberAdapter;
import co.kr.mini_spring.global.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 API 컨트롤러 (OAuth2 전용)
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "인증", description = "토큰 관리 및 로그아웃")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "토큰 재발급", description = "RefreshToken을 검증해 새로운 Access/Refresh 토큰을 발급하고 쿠키를 갱신합니다.")
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody(required = false) TokenRefreshRequest request,
                                             HttpServletRequest httpRequest,
                                             HttpServletResponse httpResponse) {
        log.info("[TokenRefresh] 요청 수신");
        
        // 1. 요청 바디 또는 쿠키에서 Refresh Token 추출
        String refreshToken = (request != null) ? request.getRefreshToken() : null;
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshToken = CookieUtil.getCookie(httpRequest, CookieUtil.REFRESH_TOKEN_NAME)
                    .map(jakarta.servlet.http.Cookie::getValue)
                    .orElse(null);
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            return ApiResponse.fail(ResponseCode.TOKEN_NOT_FOUND);
        }

        // 2. 서비스 로직 수행
        TokenResponse response = authService.refreshToken(refreshToken);
        
        // 3. 응답 쿠키 설정
        CookieUtil.setAuthCookies(httpRequest, httpResponse, response);
        
        return ApiResponse.success(ResponseCode.SUCCESS, response);
    }

    @Operation(summary = "로그아웃", description = "토큰을 무효화하고 인증 쿠키를 삭제합니다.")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal MemberAdapter memberAdapter,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        if (memberAdapter == null) {
            return ApiResponse.fail(ResponseCode.UNAUTHENTICATED);
        }

        String email = memberAdapter.getUsername();
        String accessToken = CookieUtil.resolveAccessToken(request);
        
        log.info("[Logout] 요청 수행 email={}", email);

        // 1. 서비스에서 블랙리스트 등록 및 Refresh Token 폐기
        authService.logout(email, accessToken);
        
        // 2. 클라이언트 쿠키 정리
        CookieUtil.clearAuthCookies(request, response);
        
        return ApiResponse.success();
    }
}