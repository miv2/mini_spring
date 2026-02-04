package co.kr.mini_spring.global.common.response;

import lombok.Getter;
import java.util.List;

@Getter
public class CursorResponse<T> {
    private final List<T> content;
    private final Long nextCursor;
    private final boolean hasNext;

    public CursorResponse(List<T> content, Long nextCursor, boolean hasNext) {
        this.content = content;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }
}
