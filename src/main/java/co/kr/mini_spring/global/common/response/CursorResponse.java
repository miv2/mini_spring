package co.kr.mini_spring.global.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import java.util.List;

@Getter
@Schema(description = "커서 기반 페이징 공통 응답 객체")
public class CursorResponse<T> {
    @Schema(description = "데이터 목록")
    private final List<T> content;
    
    @Schema(description = "다음 커서 (마지막 페이지인 경우 null)", example = "100")
    private final Long nextCursor;
    
    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private final boolean hasNext;

    public CursorResponse(List<T> content, Long nextCursor, boolean hasNext) {
        this.content = content;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }
}
