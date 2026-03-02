package co.kr.mini_spring.chat.dto.response;

import co.kr.mini_spring.global.common.response.ResponseCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "WebSocket(STOMP) 에러 응답")
public class ChatWsErrorResponse {

    @Schema(description = "에러 코드", example = "CHAT_NOT_PARTICIPANT")
    private final String code;
    @Schema(description = "에러 메시지", example = "채팅방 참여자가 아닙니다.")
    private final String message;

    public ChatWsErrorResponse(ResponseCode responseCode, String message) {
        this.code = responseCode.getCode();
        this.message = message == null || message.isBlank() ? responseCode.getMessage() : message;
    }

    public ChatWsErrorResponse(ResponseCode responseCode) {
        this(responseCode, responseCode.getMessage());
    }
}
