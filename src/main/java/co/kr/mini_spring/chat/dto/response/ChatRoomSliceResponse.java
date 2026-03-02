package co.kr.mini_spring.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "채팅방 커서 기반 슬라이스 응답")
public class ChatRoomSliceResponse {

    @Schema(description = "채팅방 목록")
    private final List<ChatRoomResponse> content;
    @Schema(description = "다음 조회 커서(roomId). 없으면 null", example = "17")
    private final Long nextCursor;
    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private final boolean hasNext;

    public ChatRoomSliceResponse(List<ChatRoomResponse> content, Long nextCursor, boolean hasNext) {
        this.content = content;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }
}
