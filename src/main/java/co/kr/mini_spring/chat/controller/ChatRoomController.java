package co.kr.mini_spring.chat.controller;

import co.kr.mini_spring.chat.dto.request.CreateDirectRoomRequest;
import co.kr.mini_spring.chat.dto.request.CreateGroupRoomRequest;
import co.kr.mini_spring.chat.dto.request.InviteUserRequest;
import co.kr.mini_spring.chat.dto.request.KickUserRequest;
import co.kr.mini_spring.chat.dto.response.ChatRoomResponse;
import co.kr.mini_spring.chat.dto.response.ChatRoomSliceResponse;
import co.kr.mini_spring.chat.service.ChatRoomService;
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
@RequestMapping("/api/chat/rooms")
@Tag(name = "채팅방", description = "채팅방 생성/조회/참여/초대/강퇴/밴 관리")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping("/direct")
    @Operation(summary = "1:1 채팅방 생성", description = "이미 존재하면 기존 방을 반환합니다.")
    public ApiResponse<ChatRoomResponse> createDirectRoom(
            @Valid @RequestBody CreateDirectRoomRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        return ApiResponse.success(chatRoomService.createDirectRoom(currentUserId(memberAdapter), request));
    }

    @PostMapping("/group")
    @Operation(summary = "그룹 채팅방 생성", description = "요청 사용자를 방장(owner)으로 생성합니다.")
    public ApiResponse<ChatRoomResponse> createGroupRoom(
            @Valid @RequestBody CreateGroupRoomRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        return ApiResponse.success(chatRoomService.createGroupRoom(currentUserId(memberAdapter), request));
    }

    @GetMapping
    @Operation(summary = "내 채팅방 목록", description = "미읽음 수/마지막 메시지 기준으로 정렬된 목록을 반환합니다.")
    public ApiResponse<ChatRoomSliceResponse> getMyRooms(
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        return ApiResponse.success(chatRoomService.getMyRooms(currentUserId(memberAdapter), cursor, size));
    }

    @GetMapping("/public")
    @Operation(summary = "공개 그룹 목록", description = "입장 가능한 공개 그룹 목록을 반환합니다.")
    public ApiResponse<ChatRoomSliceResponse> getPublicRooms(
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        return ApiResponse.success(chatRoomService.getPublicGroupRooms(currentUserId(memberAdapter), cursor, size));
    }

    @PostMapping("/{roomId}/join")
    @Operation(summary = "그룹 채팅방 입장")
    public ApiResponse<Void> joinRoom(
            @PathVariable Long roomId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        chatRoomService.joinGroupRoom(roomId, currentUserId(memberAdapter));
        return ApiResponse.success();
    }

    @PostMapping("/{roomId}/leave")
    @Operation(summary = "채팅방 나가기")
    public ApiResponse<Void> leaveRoom(
            @PathVariable Long roomId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        chatRoomService.leaveRoom(roomId, currentUserId(memberAdapter));
        return ApiResponse.success();
    }

    @PostMapping("/{roomId}/invite")
    @Operation(summary = "그룹 채팅방 초대", description = "방장만 호출할 수 있습니다.")
    public ApiResponse<Void> invite(
            @PathVariable Long roomId,
            @Valid @RequestBody InviteUserRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        chatRoomService.invite(roomId, currentUserId(memberAdapter), request.getTargetUserId());
        return ApiResponse.success();
    }

    @PostMapping("/{roomId}/kick")
    @Operation(summary = "그룹 채팅방 강퇴", description = "방장만 호출할 수 있습니다. 강퇴 시 밴도 함께 적용됩니다.")
    public ApiResponse<Void> kick(
            @PathVariable Long roomId,
            @Valid @RequestBody KickUserRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        chatRoomService.kick(roomId, currentUserId(memberAdapter), request.getTargetUserId());
        return ApiResponse.success();
    }

    @DeleteMapping("/{roomId}/bans/{userId}")
    @Operation(summary = "채팅방 밴 해제", description = "방장만 호출할 수 있습니다.")
    public ApiResponse<Void> unban(
            @PathVariable Long roomId,
            @PathVariable Long userId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        chatRoomService.unban(roomId, currentUserId(memberAdapter), userId);
        return ApiResponse.success();
    }

    private Long currentUserId(MemberAdapter memberAdapter) {
        if (memberAdapter == null || memberAdapter.getMember() == null) {
            throw new BusinessException(ResponseCode.UNAUTHENTICATED);
        }
        return memberAdapter.getMember().getId();
    }
}
