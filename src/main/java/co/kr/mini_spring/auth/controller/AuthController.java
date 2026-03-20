package co.kr.mini_spring.auth.controller;

import co.kr.mini_spring.auth.dto.response.TokenResponse;
import co.kr.mini_spring.auth.service.AuthService;
import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.ApiResult;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.global.security.MemberAdapter;
import co.kr.mini_spring.global.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    @Operation(summary = "토큰 재발급", description = "요청 쿠키의 refreshToken을 검증해 새로운 Access/Refresh 토큰을 발급하고 인증 쿠키를 갱신합니다.")
    @PostMapping("/refresh")
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청")
    @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰")
    public ApiResult<TokenResponse> refresh(
                                            @Parameter(hidden = true)
                                            @CookieValue(value = CookieUtil.REFRESH_TOKEN_NAME, required = false) String refreshToken,
                                            HttpServletRequest httpRequest,
                                            HttpServletResponse httpResponse) {
        log.info("[TokenRefresh] 요청 수신");

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ResponseCode.TOKEN_NOT_FOUND);
        }

        TokenResponse response = authService.refreshToken(refreshToken);
        
        CookieUtil.setAuthCookies(httpRequest, httpResponse, response);
        
        return ApiResult.success(ResponseCode.SUCCESS, response);
    }

    @Operation(summary = "로그아웃", description = "토큰을 무효화하고 인증 쿠키를 삭제합니다.")
    @PostMapping("/logout")
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    public ApiResult<Void> logout(@AuthenticationPrincipal MemberAdapter memberAdapter,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        if (memberAdapter == null) {
            return ApiResult.fail(ResponseCode.UNAUTHENTICATED);
        }

        String email = memberAdapter.getUsername();
        String accessToken = CookieUtil.resolveAccessToken(request);
        
        log.info("[Logout] 요청 수행 email={}", email);

        authService.logout(email, accessToken);
        
        CookieUtil.clearAuthCookies(request, response);
        
        return ApiResult.success();
    }
}
