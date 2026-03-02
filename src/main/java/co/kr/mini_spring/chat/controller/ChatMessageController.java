package co.kr.mini_spring.chat.controller;

import co.kr.mini_spring.chat.dto.request.MarkReadRequest;
import co.kr.mini_spring.chat.dto.response.ChatMessageSliceResponse;
import co.kr.mini_spring.chat.service.ChatMessageService;
import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.ApiResponse;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.global.security.MemberAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
@Tag(name = "채팅 메시지", description = "메시지 조회/읽음/삭제")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @GetMapping("/rooms/{roomId}/messages")
    @Operation(summary = "채팅 메시지 커서 조회", description = "cursor 미지정 시 최신 메시지부터 조회합니다.")
    public ApiResponse<ChatMessageSliceResponse> getMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        return ApiResponse.success(chatMessageService.getMessages(roomId, currentUserId(memberAdapter), cursor, size));
    }

    @PostMapping("/rooms/{roomId}/read")
    @Operation(summary = "읽음 처리", description = "사용자의 마지막 읽은 메시지 ID를 갱신합니다.")
    public ApiResponse<Void> markRead(
            @PathVariable Long roomId,
            @Valid @RequestBody MarkReadRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        chatMessageService.markAsRead(roomId, currentUserId(memberAdapter), request);
        return ApiResponse.success();
    }

    @DeleteMapping("/messages/{messageId}")
    @Operation(summary = "메시지 삭제", description = "본인 메시지에 한해 5분 내 삭제할 수 있습니다.")
    public ApiResponse<Void> deleteMessage(
            @PathVariable Long messageId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        chatMessageService.deleteMessage(messageId, currentUserId(memberAdapter));
        return ApiResponse.success();
    }

    private Long currentUserId(MemberAdapter memberAdapter) {
        if (memberAdapter == null || memberAdapter.getMember() == null) {
            throw new BusinessException(ResponseCode.UNAUTHENTICATED);
        }
        return memberAdapter.getMember().getId();
    }
}
