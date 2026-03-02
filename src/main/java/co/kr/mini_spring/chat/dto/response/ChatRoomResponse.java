package co.kr.mini_spring.chat.dto.response;

import co.kr.mini_spring.chat.domain.Conversation;
import co.kr.mini_spring.chat.domain.ConversationType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "채팅방 요약 응답")
public class ChatRoomResponse {

    @Schema(description = "채팅방 ID", example = "10")
    private final Long roomId;
    @Schema(description = "채팅방 타입", example = "GROUP")
    private final ConversationType type;
    @Schema(description = "채팅방 제목(1:1은 상대 닉네임으로 노출 가능)", example = "백엔드 스터디")
    private final String title;
    @Schema(description = "방장 사용자 ID", example = "1")
    private final Long ownerId;
    @Schema(description = "마지막 메시지 미리보기", example = "오늘 회의 8시")
    private final String lastMessagePreview;
    @Schema(description = "마지막 메시지 시각", example = "2026-03-02T10:15:30")
    private final LocalDateTime lastMessageAt;
    @Schema(description = "내 기준 미읽음 개수", example = "5")
    private final long unreadCount;
    @Schema(description = "참여자 수", example = "12")
    private final long participantCount;
    @Schema(description = "목록/헤더 표시용 이름 (DIRECT: 상대 닉네임, GROUP: 채팅방 제목)", example = "miv")
    private final String displayName;
    @Schema(description = "목록/헤더 표시용 프로필 이미지 URL (DIRECT에서 상대 프로필)", example = "https://minispring.duckdns.org/uploads/2026/03/02/profile.png")
    private final String profileImageUrl;

    public ChatRoomResponse(Conversation conversation) {
        this(
                conversation.getId(),
                conversation.getType(),
                conversation.getTitle(),
                conversation.getOwnerId(),
                conversation.getLastMessagePreview(),
                conversation.getLastMessageAt(),
                0L,
                0L,
                conversation.getTitle(),
                null
        );
    }

    public ChatRoomResponse(Long roomId, ConversationType type, String title, Long ownerId,
                            String lastMessagePreview, LocalDateTime lastMessageAt,
                            Long unreadCount, Long participantCount) {
        this(roomId, type, title, ownerId, lastMessagePreview, lastMessageAt, unreadCount, participantCount, title, null);
    }

    public ChatRoomResponse(Long roomId, ConversationType type, String title, Long ownerId,
                            String lastMessagePreview, LocalDateTime lastMessageAt,
                            Long unreadCount, Long participantCount,
                            String displayName, String profileImageUrl) {
        this.roomId = roomId;
        this.type = type;
        this.title = title;
        this.ownerId = ownerId;
        this.lastMessagePreview = lastMessagePreview;
        this.lastMessageAt = lastMessageAt;
        this.unreadCount = unreadCount == null ? 0 : unreadCount;
        this.participantCount = participantCount == null ? 0 : participantCount;
        this.displayName = displayName;
        this.profileImageUrl = profileImageUrl;
    }
}
