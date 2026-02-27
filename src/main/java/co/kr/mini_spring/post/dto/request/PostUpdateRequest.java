package co.kr.mini_spring.post.dto.request;

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

    @Schema(description = "해시태그 목록 (전체 대체)", example = "[\"java\", \"backend\"]")
    private List<String> hashtags;
}
