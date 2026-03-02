package co.kr.mini_spring.chat.dto.response;

import co.kr.mini_spring.global.common.response.ResponseCode;
import lombok.Getter;

@Getter
public class ChatWsErrorResponse {

    private final String code;
    private final String message;

    public ChatWsErrorResponse(ResponseCode responseCode, String message) {
        this.code = responseCode.getCode();
        this.message = message == null || message.isBlank() ? responseCode.getMessage() : message;
    }

    public ChatWsErrorResponse(ResponseCode responseCode) {
        this(responseCode, responseCode.getMessage());
    }
}

