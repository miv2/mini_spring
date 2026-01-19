package co.kr.mini_spring.member.controller;

import co.kr.mini_spring.global.common.response.ApiResponse;
import co.kr.mini_spring.global.security.MemberAdapter;
import co.kr.mini_spring.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
@Tag(name = "회원", description = "회원 정보 관리 API")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "프로필 이미지 수정", description = "현재 로그인한 사용자의 프로필 이미지를 업데이트합니다.")
    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> updateProfileImage(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter,
            @RequestParam("file") MultipartFile file
    ) {
        if (memberAdapter == null) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }

        Long memberId = memberAdapter.getMember().getId();
        log.info("[UpdateProfileImage] 요청 memberId={}, fileName={}", memberId, file.getOriginalFilename());
        
        String imageUrl = memberService.updateProfileImage(memberId, file);
        return ApiResponse.success(imageUrl);
    }
}