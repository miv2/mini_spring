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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 API 컨트롤러 (OAuth2 전용)
 * - 소셜 로그인은 /oauth2/authorization/{provider}를 통해 처리
 * - 이 컨트롤러는 토큰 갱신 및 로그아웃만 담당
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "인증", description = "토큰 관리 (OAuth2 전용)")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "토큰 재발급", description = "RefreshToken을 검증해 새로운 Access/Refresh 토큰을 발급합니다.")
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        log.info("[TokenRefresh] 요청");
        TokenResponse response = authService.refreshToken(request.getRefreshToken());
        return ApiResponse.success(ResponseCode.SUCCESS, response);
    }

    @Operation(summary = "로그아웃", description = "로그인된 사용자의 RefreshToken을 폐기합니다.")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal MemberAdapter memberAdapter) {
        log.info("[Logout] 요청 email={}", memberAdapter.getUsername());
        authService.logout(memberAdapter.getUsername());
        return ApiResponse.success();
    }

}
