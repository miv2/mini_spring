package co.kr.mini_spring.chat.dto.request;

import co.kr.mini_spring.chat.domain.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ChatSendMessageRequest {

    @NotBlank(message = "클라이언트 메시지 ID는 필수입니다.")
    @Size(max = 100, message = "클라이언트 메시지 ID는 100자를 초과할 수 없습니다.")
    private String clientMessageId;

    @NotNull(message = "메시지 타입은 필수입니다.")
    private MessageType type = MessageType.TEXT;

    @NotBlank(message = "메시지 내용은 필수입니다.")
    @Size(max = 1000, message = "메시지 길이는 1000자를 초과할 수 없습니다.")
    private String content;
}

