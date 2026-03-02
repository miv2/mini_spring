package co.kr.mini_spring.chat.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class ChatMessageSliceResponse {

    private final List<ChatMessageResponse> messages;
    private final Long nextCursor;
    private final boolean hasNext;

    public ChatMessageSliceResponse(List<ChatMessageResponse> messages, Long nextCursor, boolean hasNext) {
        this.messages = messages;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }
}

