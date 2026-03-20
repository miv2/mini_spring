package co.kr.mini_spring.chat.controller;

import co.kr.mini_spring.chat.service.ChatBlockService;
import co.kr.mini_spring.global.common.response.ApiResult;
import co.kr.mini_spring.global.security.AuthUtils;
import co.kr.mini_spring.global.security.MemberAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/blocks")
@Tag(name = "채팅 차단", description = "사용자 차단/해제")
public class ChatBlockController {

    private final ChatBlockService chatBlockService;

    @PostMapping("/{userId}")
    @Operation(summary = "사용자 차단")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiResult<Void> block(
            @PathVariable Long userId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        chatBlockService.blockUser(currentUserId(memberAdapter), userId);
        return ApiResult.success();
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "사용자 차단 해제")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiResult<Void> unblock(
            @PathVariable Long userId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        chatBlockService.unblockUser(currentUserId(memberAdapter), userId);
        return ApiResult.success();
    }

    private Long currentUserId(MemberAdapter memberAdapter) {
        return AuthUtils.currentUserId(memberAdapter);
    }
}
