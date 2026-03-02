package co.kr.mini_spring.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "채팅 메시지 커서 기반 슬라이스 응답")
public class ChatMessageSliceResponse {

    @Schema(description = "메시지 목록")
    private final List<ChatMessageResponse> messages;
    @Schema(description = "다음 조회 커서(messageId). 없으면 null", example = "87")
    private final Long nextCursor;
    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private final boolean hasNext;

    public ChatMessageSliceResponse(List<ChatMessageResponse> messages, Long nextCursor, boolean hasNext) {
        this.messages = messages;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }
}
