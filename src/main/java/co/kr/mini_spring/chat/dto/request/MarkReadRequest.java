package co.kr.mini_spring.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "채팅방 읽음 처리 요청")
public class MarkReadRequest {

    @Schema(description = "마지막으로 읽은 메시지 ID", example = "120")
    @NotNull(message = "마지막 읽은 메시지 ID는 필수입니다.")
    private Long lastReadMessageId;
}
