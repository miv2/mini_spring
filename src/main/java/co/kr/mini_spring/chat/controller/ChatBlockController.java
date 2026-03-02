package co.kr.mini_spring.chat.controller;

import co.kr.mini_spring.chat.service.ChatBlockService;
import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.ApiResponse;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.global.security.MemberAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    public ApiResponse<Void> block(
            @PathVariable Long userId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        chatBlockService.blockUser(currentUserId(memberAdapter), userId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "사용자 차단 해제")
    public ApiResponse<Void> unblock(
            @PathVariable Long userId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        chatBlockService.unblockUser(currentUserId(memberAdapter), userId);
        return ApiResponse.success();
    }

    private Long currentUserId(MemberAdapter memberAdapter) {
        if (memberAdapter == null || memberAdapter.getMember() == null) {
            throw new BusinessException(ResponseCode.UNAUTHENTICATED);
        }
        return memberAdapter.getMember().getId();
    }
}
