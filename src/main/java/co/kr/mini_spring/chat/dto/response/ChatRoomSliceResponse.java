package co.kr.mini_spring.chat.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class ChatRoomSliceResponse {

    private final List<ChatRoomResponse> content;
    private final Long nextCursor;
    private final boolean hasNext;

    public ChatRoomSliceResponse(List<ChatRoomResponse> content, Long nextCursor, boolean hasNext) {
        this.content = content;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }
}
