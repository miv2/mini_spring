package co.kr.mini_spring.chat.dto.response;

import co.kr.mini_spring.chat.domain.Message;
import co.kr.mini_spring.chat.domain.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "채팅 메시지 응답")
public class ChatMessageResponse {
    private static final String CREATED_EVENT = "MESSAGE";
    private static final String DELETED_EVENT = "MESSAGE_DELETED";
    private static final String DELETED_CONTENT = "삭제된 메시지입니다.";

    @Schema(description = "메시지 ID", example = "101")
    private final Long messageId;
    @Schema(description = "채팅방 ID", example = "10")
    private final Long roomId;
    @Schema(description = "발신자 사용자 ID", example = "1")
    private final Long senderId;
    @Schema(description = "클라이언트 메시지 ID", example = "web-1700000000000-1")
    private final String clientMessageId;
    @Schema(description = "메시지 타입", example = "TEXT")
    private final MessageType type;
    @Schema(description = "메시지 본문", example = "안녕하세요.")
    private final String content;
    @Schema(description = "생성 시각", example = "2026-03-02T10:15:30")
    private final LocalDateTime createdAt;
    @Schema(description = "삭제 여부", example = "false")
    private final boolean deleted;
    @Schema(description = "이벤트 타입", example = "MESSAGE")
    private final String eventType;
    @Schema(description = "삭제 처리한 사용자 ID (삭제 이벤트에서만 존재)", example = "1")
    private final Long deletedBy;
    @Schema(description = "삭제 시각 (삭제 이벤트에서만 존재)", example = "2026-03-02T10:20:00")
    private final LocalDateTime deletedAt;
    @Schema(description = "발신자 닉네임", example = "miv")
    private final String senderNickname;
    @Schema(description = "발신자 프로필 이미지 URL", example = "https://minispring.duckdns.org/uploads/2026/03/02/profile.png")
    private final String senderProfileImageUrl;

    public static ChatMessageResponse of(Message message,
                                         String senderNickname,
                                         String senderProfileImageUrl) {
        return new ChatMessageResponse(
                message,
                message.getDeletedAt() != null ? DELETED_EVENT : CREATED_EVENT,
                null,
                senderNickname,
                senderProfileImageUrl
        );
    }

    public static ChatMessageResponse deleted(Message message,
                                              Long deletedBy,
                                              String senderNickname,
                                              String senderProfileImageUrl) {
        return new ChatMessageResponse(message, DELETED_EVENT, deletedBy, senderNickname, senderProfileImageUrl);
    }

    private ChatMessageResponse(Message message,
                                String eventType,
                                Long deletedBy,
                                String senderNickname,
                                String senderProfileImageUrl) {
        this.messageId = message.getId();
        this.roomId = message.getConversationId();
        this.senderId = message.getSenderId();
        this.clientMessageId = message.getClientMessageId();
        this.type = message.getType();
        this.content = message.getDeletedAt() != null ? DELETED_CONTENT : message.getContent();
        this.createdAt = message.getCreatedAt();
        this.deleted = message.getDeletedAt() != null;
        this.eventType = eventType;
        this.deletedBy = deletedBy;
        this.deletedAt = message.getDeletedAt();
        this.senderNickname = senderNickname;
        this.senderProfileImageUrl = senderProfileImageUrl;
    }
}
