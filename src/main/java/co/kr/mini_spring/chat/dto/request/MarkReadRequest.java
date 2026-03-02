package co.kr.mini_spring.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class MarkReadRequest {

    @NotNull(message = "마지막 읽은 메시지 ID는 필수입니다.")
    private Long lastReadMessageId;
}

