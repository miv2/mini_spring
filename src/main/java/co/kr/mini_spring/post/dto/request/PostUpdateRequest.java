package co.kr.mini_spring.post.dto.request;

import co.kr.mini_spring.post.validation.ValidHashtags;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "게시글 수정 요청")
public class PostUpdateRequest {

    @Schema(description = "수정할 제목", example = "수정된 게시글 제목")
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @Schema(description = "수정할 내용", example = "수정된 게시글 본문 내용입니다.")
    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @Schema(description = "해시태그 목록 (빈 배열이면 전체 제거, null이면 기존 유지. 영문, 숫자, 완성형 한글만 허용. `ㅋㅋ`, `ㅎㅎ` 같은 자모-only 태그는 불가)", example = "[\"java\", \"backend\"]")
    @ValidHashtags
    private List<String> hashtags;
}
