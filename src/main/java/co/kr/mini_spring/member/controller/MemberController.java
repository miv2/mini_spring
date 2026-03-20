package co.kr.mini_spring.member.controller;

import co.kr.mini_spring.global.common.response.ApiResult;
import co.kr.mini_spring.global.security.AuthUtils;
import co.kr.mini_spring.global.security.MemberAdapter;
import co.kr.mini_spring.member.dto.response.MemberResponse;
import co.kr.mini_spring.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
@Tag(name = "회원", description = "회원 정보 관리 API")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("/me")
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    public ApiResult<MemberResponse> getMyInfo(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter
    ) {
        Long memberId = AuthUtils.currentUserId(memberAdapter);
        log.info("[GetMyInfo] 요청 memberId={}", memberId);
        return ApiResult.success(memberService.getMyInfo(memberId));
    }

    @Operation(summary = "프로필 이미지 수정", description = "현재 로그인한 사용자의 프로필 이미지를 업데이트합니다.")
    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiResponse(responseCode = "401", description = "인증 필요")
    public ApiResult<String> updateProfileImage(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter,
            @RequestParam("file") MultipartFile file
    ) {
        Long memberId = AuthUtils.currentUserId(memberAdapter);
        log.info("[UpdateProfileImage] 요청 memberId={}, fileName={}", memberId, file.getOriginalFilename());
        return ApiResult.success(memberService.updateProfileImage(memberId, file));
    }
}
