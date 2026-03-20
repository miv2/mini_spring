package co.kr.mini_spring.chat.controller;

import co.kr.mini_spring.chat.dto.request.CreateDirectRoomRequest;
import co.kr.mini_spring.chat.dto.request.CreateGroupRoomRequest;
import co.kr.mini_spring.chat.dto.request.InviteUserRequest;
import co.kr.mini_spring.chat.dto.request.KickUserRequest;
import co.kr.mini_spring.chat.dto.response.ChatParticipantResponse;
import co.kr.mini_spring.chat.dto.response.ChatBanResponse;
import co.kr.mini_spring.chat.dto.response.ChatRoomResponse;
import co.kr.mini_spring.chat.dto.response.ChatRoomSliceResponse;
import co.kr.mini_spring.chat.service.ChatRoomService;
import java.util.List;
import co.kr.mini_spring.global.common.response.ApiResult;
import co.kr.mini_spring.global.security.AuthUtils;
import co.kr.mini_spring.global.security.MemberAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiResult<ChatRoomResponse> createDirectRoom(
            @Valid @RequestBody CreateDirectRoomRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        return ApiResult.success(chatRoomService.createDirectRoom(currentUserId(memberAdapter), request));
    }

    @PostMapping("/group")
    @Operation(summary = "그룹 채팅방 생성", description = "요청 사용자를 방장(owner)으로 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiResult<ChatRoomResponse> createGroupRoom(
            @Valid @RequestBody CreateGroupRoomRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        return ApiResult.success(chatRoomService.createGroupRoom(currentUserId(memberAdapter), request));
    }

    @GetMapping
    @Operation(summary = "내 채팅방 목록", description = "미읽음 수/마지막 메시지 기준으로 정렬된 목록을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiResult<ChatRoomSliceResponse> getMyRooms(
            @Parameter(description = "마지막으로 받은 roomId (없으면 최신부터)", example = "30")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "조회 개수 (기본 20, 최대 100)", example = "20")
            @RequestParam(required = false) Integer size,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        return ApiResult.success(chatRoomService.getMyRooms(currentUserId(memberAdapter), cursor, size));
    }

    @GetMapping("/public")
    @Operation(summary = "공개 그룹 목록", description = "입장 가능한 공개 그룹 목록을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiResult<ChatRoomSliceResponse> getPublicRooms(
            @Parameter(description = "마지막으로 받은 roomId (없으면 최신부터)", example = "30")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "조회 개수 (기본 20, 최대 100)", example = "20")
            @RequestParam(required = false) Integer size,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        return ApiResult.success(chatRoomService.getPublicGroupRooms(currentUserId(memberAdapter), cursor, size));
    }

    @PostMapping("/{roomId}/join")
    @Operation(summary = "그룹 채팅방 입장")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "정원 초과/밴 상태"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "채팅방 없음")
    })
    public ApiResult<Void> joinRoom(
            @PathVariable Long roomId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        chatRoomService.joinGroupRoom(roomId, currentUserId(memberAdapter));
        return ApiResult.success();
    }

    @PostMapping("/{roomId}/leave")
    @Operation(summary = "채팅방 나가기")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "참여 중인 방이 아님")
    })
    public ApiResult<Void> leaveRoom(
            @PathVariable Long roomId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        chatRoomService.leaveRoom(roomId, currentUserId(memberAdapter));
        return ApiResult.success();
    }

    @PostMapping("/{roomId}/invite")
    @Operation(summary = "그룹 채팅방 초대", description = "방장만 호출할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "채팅방 없음")
    })
    public ApiResult<Void> invite(
            @PathVariable Long roomId,
            @Valid @RequestBody InviteUserRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        chatRoomService.invite(roomId, currentUserId(memberAdapter), request.getTargetUserId());
        return ApiResult.success();
    }

    @PostMapping("/{roomId}/kick")
    @Operation(summary = "그룹 채팅방 강퇴", description = "방장만 호출할 수 있습니다. 강퇴 시 밴도 함께 적용됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "채팅방 없음")
    })
    public ApiResult<Void> kick(
            @PathVariable Long roomId,
            @Valid @RequestBody KickUserRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        chatRoomService.kick(roomId, currentUserId(memberAdapter), request.getTargetUserId());
        return ApiResult.success();
    }

    @DeleteMapping("/{roomId}/bans/{userId}")
    @Operation(summary = "채팅방 밴 해제", description = "방장만 호출할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "채팅방 없음")
    })
    public ApiResult<Void> unban(
            @PathVariable Long roomId,
            @PathVariable Long userId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        chatRoomService.unban(roomId, currentUserId(memberAdapter), userId);
        return ApiResult.success();
    }

    @GetMapping("/{roomId}/participants")
    @Operation(summary = "채팅방 참여자 목록 조회", description = "참여자만 호출할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "채팅방 없음")
    })
    public ApiResult<List<ChatParticipantResponse>> getParticipants(
            @PathVariable Long roomId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        return ApiResult.success(chatRoomService.getParticipants(roomId, currentUserId(memberAdapter)));
    }

    @GetMapping("/{roomId}/bans")
    @Operation(summary = "채팅방 밴(강퇴) 목록 조회", description = "방장만 호출할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "채팅방 없음")
    })
    public ApiResult<List<ChatBanResponse>> getBans(
            @PathVariable Long roomId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberAdapter memberAdapter) {
        return ApiResult.success(chatRoomService.getBans(roomId, currentUserId(memberAdapter)));
    }

    private Long currentUserId(MemberAdapter memberAdapter) {
        return AuthUtils.currentUserId(memberAdapter);
    }
}
