package co.kr.mini_spring.auth.controller;

import co.kr.mini_spring.global.security.MemberAdapter;
import co.kr.mini_spring.auth.dto.request.LoginRequest;
import co.kr.mini_spring.auth.dto.request.TokenRefreshRequest;
import co.kr.mini_spring.auth.dto.response.TokenResponse;
import co.kr.mini_spring.member.dto.request.SignUpRequest;
import co.kr.mini_spring.member.dto.response.SignUpResponse;
import co.kr.mini_spring.global.common.response.ApiResponse;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.member.service.MemberService;
import co.kr.mini_spring.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 API 컨트롤러
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "인증", description = "회원가입/로그인/토큰 관리")
public class AuthController {

    private final MemberService memberService;
    private final AuthService authService;

    @Operation(summary = "회원가입", description = "이메일/비밀번호로 회원가입하고 Access/Refresh 토큰을 발급합니다.")
    @PostMapping("/signup")
    public ApiResponse<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        log.info("[SignUp] 요청 email={}", request.getEmail());
        SignUpResponse response = memberService.signUp(request);
        return ApiResponse.success(ResponseCode.CREATED, response);
    }

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하고 Access/Refresh 토큰을 발급합니다.")
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("[Login] 요청 email={}", request.getEmail());
        TokenResponse response = authService.login(request);
        return ApiResponse.success(ResponseCode.SUCCESS, response);
    }

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
