package co.kr.mini_spring.chat.dto.response;

import co.kr.mini_spring.chat.domain.Conversation;
import co.kr.mini_spring.chat.domain.ConversationType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRoomResponse {

    private final Long roomId;
    private final ConversationType type;
    private final String title;
    private final Long ownerId;
    private final String lastMessagePreview;
    private final LocalDateTime lastMessageAt;
    private final long unreadCount;
    private final long participantCount;

    public ChatRoomResponse(Conversation conversation) {
        this.roomId = conversation.getId();
        this.type = conversation.getType();
        this.title = conversation.getTitle();
        this.ownerId = conversation.getOwnerId();
        this.lastMessagePreview = conversation.getLastMessagePreview();
        this.lastMessageAt = conversation.getLastMessageAt();
        this.unreadCount = 0;
        this.participantCount = 0;
    }

    public ChatRoomResponse(Long roomId, ConversationType type, String title, Long ownerId,
                            String lastMessagePreview, LocalDateTime lastMessageAt,
                            Long unreadCount, Long participantCount) {
        this.roomId = roomId;
        this.type = type;
        this.title = title;
        this.ownerId = ownerId;
        this.lastMessagePreview = lastMessagePreview;
        this.lastMessageAt = lastMessageAt;
        this.unreadCount = unreadCount == null ? 0 : unreadCount;
        this.participantCount = participantCount == null ? 0 : participantCount;
    }
}

