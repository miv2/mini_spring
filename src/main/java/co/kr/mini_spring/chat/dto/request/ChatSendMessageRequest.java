package co.kr.mini_spring.chat.dto.request;

import co.kr.mini_spring.chat.domain.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "채팅 메시지 전송 요청")
public class ChatSendMessageRequest {

    @Schema(description = "클라이언트에서 생성한 메시지 고유 ID(중복 전송 방지)", example = "web-1700000000000-1")
    @NotBlank(message = "클라이언트 메시지 ID는 필수입니다.")
    @Size(max = 100, message = "클라이언트 메시지 ID는 100자를 초과할 수 없습니다.")
    private String clientMessageId;

    @Schema(description = "메시지 타입", example = "TEXT")
    @NotNull(message = "메시지 타입은 필수입니다.")
    private MessageType type = MessageType.TEXT;

    @Schema(description = "메시지 내용", example = "안녕하세요.")
    @NotBlank(message = "메시지 내용은 필수입니다.")
    @Size(max = 1000, message = "메시지 길이는 1000자를 초과할 수 없습니다.")
    private String content;
}
