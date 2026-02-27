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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "인증", description = "토큰 관리 및 로그아웃")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "토큰 재발급", description = "RefreshToken을 검증해 새로운 Access/Refresh 토큰을 발급하고 쿠키를 갱신합니다.")
    @PostMapping("/refresh")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody(required = false) TokenRefreshRequest request,
                                             HttpServletRequest httpRequest,
                                             HttpServletResponse httpResponse) {
        log.info("[TokenRefresh] 요청 수신");
        
        String refreshToken = (request != null) ? request.getRefreshToken() : null;
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshToken = CookieUtil.getCookie(httpRequest, CookieUtil.REFRESH_TOKEN_NAME)
                    .map(jakarta.servlet.http.Cookie::getValue)
                    .orElse(null);
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            return ApiResponse.fail(ResponseCode.TOKEN_NOT_FOUND);
        }

        TokenResponse response = authService.refreshToken(refreshToken);
        
        CookieUtil.setAuthCookies(httpRequest, httpResponse, response);
        
        return ApiResponse.success(ResponseCode.SUCCESS, response);
    }

    @Operation(summary = "로그아웃", description = "토큰을 무효화하고 인증 쿠키를 삭제합니다.")
    @PostMapping("/logout")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    public ApiResponse<Void> logout(@AuthenticationPrincipal MemberAdapter memberAdapter,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        if (memberAdapter == null) {
            return ApiResponse.fail(ResponseCode.UNAUTHENTICATED);
        }

        String email = memberAdapter.getUsername();
        String accessToken = CookieUtil.resolveAccessToken(request);
        
        log.info("[Logout] 요청 수행 email={}", email);

        authService.logout(email, accessToken);
        
        CookieUtil.clearAuthCookies(request, response);
        
        return ApiResponse.success();
    }
}
