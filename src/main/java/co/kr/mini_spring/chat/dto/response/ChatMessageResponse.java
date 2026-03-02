package co.kr.mini_spring.chat.dto.response;

import co.kr.mini_spring.chat.domain.Message;
import co.kr.mini_spring.chat.domain.MessageType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatMessageResponse {

    private final Long messageId;
    private final Long roomId;
    private final Long senderId;
    private final String clientMessageId;
    private final MessageType type;
    private final String content;
    private final LocalDateTime createdAt;
    private final boolean deleted;

    public ChatMessageResponse(Message message) {
        this.messageId = message.getId();
        this.roomId = message.getConversationId();
        this.senderId = message.getSenderId();
        this.clientMessageId = message.getClientMessageId();
        this.type = message.getType();
        this.content = message.getContent();
        this.createdAt = message.getCreatedAt();
        this.deleted = message.getDeletedAt() != null;
    }
}

